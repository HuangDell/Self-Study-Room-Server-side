package com.studyroom.service;

import com.studyroom.dto.BookingRequest;
import com.studyroom.dto.SeatRequest;
import com.studyroom.model.*;
import com.studyroom.repository.BookingRepository;
import com.studyroom.repository.RoomRepository;
import com.studyroom.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SeatServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private SeatService seatService;

    private Seat testSeat;
    private Room testRoom;
    private Student testStudent;
    private SeatRequest seatRequest;
    private BookingRequest bookingRequest;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setName("Test Room");
        testRoom.setType(0); // General room type

        testSeat = new Seat();
        testSeat.setId(1L);
        testSeat.setSeatNumber("A1");
        testSeat.setSeatName("A1");
        testSeat.setRoom(testRoom);
        testSeat.setStatus(Seat.SeatStatus.AVAILABLE);
        testSeat.setHasSocket(true);

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setUsername("teststudent");
        testStudent.setType(0); // Student type matching general room

        seatRequest = new SeatRequest();
        seatRequest.setRoomId(testRoom.getId());
        seatRequest.setSeatName("A2");
        seatRequest.setHasSocket(false);
        // seatRequest.setStatus(Seat.SeatStatus.AVAILABLE); // Status is set in updateSeat if provided

        bookingRequest = new BookingRequest();
        bookingRequest.setSeatId(testSeat.getId());
        bookingRequest.setStartTime(Instant.now().toEpochMilli());
        bookingRequest.setEndTime(Instant.now().plusSeconds(3600).toEpochMilli());

        testBooking = new Booking();
        testBooking.setId(1L);
        testBooking.setStudent(testStudent);
        testBooking.setSeat(testSeat);
        testBooking.setRoom(testRoom);
        testBooking.setStatus(1); // Active booking
        testBooking.setStartTime(Instant.now().minusSeconds(1800)); // Booking started some time ago
        testBooking.setEndTime(Instant.now().plusSeconds(1800));  // Booking ends some time in future
    }

    @Test
    void addSeat_ShouldCreateSeat_WhenRoomExistsAndSeatNotExists() {
        when(roomRepository.findById(seatRequest.getRoomId())).thenReturn(Optional.of(testRoom));
        when(seatRepository.findByRoomAndSeatNumber(testRoom, seatRequest.getSeatName())).thenReturn(Optional.empty());
        when(seatRepository.save(any(Seat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Seat createdSeat = seatService.addSeat(seatRequest);

        assertNotNull(createdSeat);
        assertEquals(seatRequest.getSeatName(), createdSeat.getSeatName());
        assertEquals(testRoom, createdSeat.getRoom());
        assertEquals(seatRequest.getHasSocket(), createdSeat.isHasSocket());
        verify(seatRepository).save(any(Seat.class));
    }

    @Test
    void addSeat_ShouldThrowException_WhenRoomNotFound() {
        when(roomRepository.findById(seatRequest.getRoomId())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> seatService.addSeat(seatRequest));
        assertEquals("Room not found", exception.getMessage());
        verify(seatRepository, never()).save(any(Seat.class));
    }

    @Test
    void addSeat_ShouldThrowException_WhenSeatAlreadyExists() {
        when(roomRepository.findById(seatRequest.getRoomId())).thenReturn(Optional.of(testRoom));
        when(seatRepository.findByRoomAndSeatNumber(testRoom, seatRequest.getSeatName())).thenReturn(Optional.of(new Seat()));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> seatService.addSeat(seatRequest));
        assertEquals("Seat already exists in this room", exception.getMessage());
        verify(seatRepository, never()).save(any(Seat.class));
    }

    @Test
    void bookSeat_ShouldBookSeat_WhenSeatAvailableAndStudentTypeMatches() throws NoResourceFoundException {
        when(seatRepository.findById(bookingRequest.getSeatId())).thenReturn(Optional.of(testSeat));
        when(seatRepository.save(any(Seat.class))).thenReturn(testSeat);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        seatService.bookSeat(testStudent, bookingRequest);

        assertEquals(Seat.SeatStatus.OCCUPIED, testSeat.getStatus());
        verify(seatRepository).save(testSeat);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void bookSeat_ShouldThrowNoResourceFoundException_WhenSeatNotFound() {
        when(seatRepository.findById(bookingRequest.getSeatId())).thenReturn(Optional.empty());

        NoResourceFoundException exception = assertThrows(NoResourceFoundException.class,
                () -> seatService.bookSeat(testStudent, bookingRequest));
        assertEquals("No static resource Seat not found.", exception.getMessage()); // Updated expected message
        assertEquals(HttpMethod.POST, exception.getHttpMethod());
    }

    @Test
    void bookSeat_ShouldThrowAccessDeniedException_WhenRoomTypeNotMatchStudentType() {
        testRoom.setType(1); // Specific room type
        testStudent.setType(2); // Different student type
        when(seatRepository.findById(bookingRequest.getSeatId())).thenReturn(Optional.of(testSeat));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> seatService.bookSeat(testStudent, bookingRequest));
        assertEquals("This room is not open to you", exception.getMessage());
    }

    @Test
    void bookSeat_ShouldThrowRuntimeException_WhenSeatNotAvailable() {
        testSeat.setStatus(Seat.SeatStatus.OCCUPIED);
        when(seatRepository.findById(bookingRequest.getSeatId())).thenReturn(Optional.of(testSeat));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> seatService.bookSeat(testStudent, bookingRequest));
        assertEquals("Seat is not available", exception.getMessage());
    }


    @Test
    void cancelBooking_ShouldCancelBooking_WhenBookingExists() {
        when(bookingRepository.findByIdAndStudent(testBooking.getId(), testStudent)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        seatService.cancelBooking(testStudent, testBooking.getId());

        assertEquals(0, testBooking.getStatus()); // 0 for CANCELLED
        verify(bookingRepository).save(testBooking);
    }

    @Test
    void cancelBooking_ShouldThrowException_WhenBookingNotFound() {
        when(bookingRepository.findByIdAndStudent(testBooking.getId(), testStudent)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> seatService.cancelBooking(testStudent, testBooking.getId()));
        assertEquals("Booking not found", exception.getMessage());
    }

    @Test
    void temporaryLeaveSeat_ShouldUpdateBookingStatus() {
        when(bookingRepository.findByStudentOrderByStartTimeDesc(testStudent))
                .thenReturn(Collections.singletonList(testBooking)); // testBooking's seat ID matches testSeat.getId()
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        seatService.temporaryLeaveSeat(testStudent, testSeat.getId());

        assertEquals(3, testBooking.getStatus()); // 3 for TEMPORARY_LEAVE
        verify(bookingRepository).save(testBooking);
    }

    @Test
    void temporaryLeaveSeat_ShouldThrowException_WhenNoActiveBookingForSeat() {
        when(bookingRepository.findByStudentOrderByStartTimeDesc(testStudent)).thenReturn(Collections.emptyList());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> seatService.temporaryLeaveSeat(testStudent, testSeat.getId()));
        assertEquals("Student has not booked this seat", exception.getMessage());
    }

    @Test
    void checkInSeat_ShouldUpdateBookingStatus() {
        when(bookingRepository.findByStudentOrderByStartTimeDesc(testStudent))
                .thenReturn(Collections.singletonList(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        seatService.checkInSeat(testStudent, testSeat.getId());

        assertEquals(2, testBooking.getStatus()); // 2 for CHECKED_IN/COMPLETED
        verify(bookingRepository).save(testBooking);
    }

    @Test
    void releaseSeat_ShouldUpdateBookingAndSeatStatus() {
        when(seatRepository.findById(testSeat.getId())).thenReturn(Optional.of(testSeat));
        when(bookingRepository.findByStudentOrderByStartTimeDesc(testStudent))
                .thenReturn(Collections.singletonList(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(seatRepository.save(any(Seat.class))).thenReturn(testSeat);

        seatService.releaseSeat(testStudent, testSeat.getId());

        assertEquals(2, testBooking.getStatus());
        assertEquals(Seat.SeatStatus.AVAILABLE, testSeat.getStatus());
        verify(bookingRepository).save(testBooking);
        verify(seatRepository).save(testSeat);
    }


    @Test
    void deleteSeat_ShouldSuccess_WhenSeatExists() {
        Long seatIdToDelete = testSeat.getId();
        when(seatRepository.findById(seatIdToDelete)).thenReturn(Optional.of(testSeat));
        doNothing().when(bookingRepository).deleteBySeatId(seatIdToDelete);
        doNothing().when(seatRepository).delete(testSeat);

        assertDoesNotThrow(() -> seatService.deleteSeat(seatIdToDelete));

        verify(bookingRepository, times(1)).deleteBySeatId(seatIdToDelete);
        verify(seatRepository, times(1)).delete(testSeat);
    }

    @Test
    void deleteSeat_ShouldThrowException_WhenSeatNotExists() {
        Long seatIdToDelete = 99L;
        when(seatRepository.findById(seatIdToDelete)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> seatService.deleteSeat(seatIdToDelete));
        assertEquals("Seat not found", exception.getMessage());
        verify(bookingRepository, never()).deleteBySeatId(anyLong());
        verify(seatRepository, never()).delete(any(Seat.class));
    }

    @Test
    void updateSeat_ShouldUpdateFields_WhenSeatExistsAndRequestFieldsNotNull() {
        Long seatIdToUpdate = testSeat.getId();
        SeatRequest updateRequest = new SeatRequest();
        updateRequest.setSeatName("UpdatedName");
        updateRequest.setHasSocket(false);
        updateRequest.setStatus(Seat.SeatStatus.UNAVAILABLE);

        when(seatRepository.findById(seatIdToUpdate)).thenReturn(Optional.of(testSeat));
        when(seatRepository.save(any(Seat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Seat updatedSeat = seatService.updateSeat(seatIdToUpdate, updateRequest);

        assertNotNull(updatedSeat);
        assertEquals("UpdatedName", updatedSeat.getSeatName());
        assertEquals(false, updatedSeat.isHasSocket());
        assertEquals(Seat.SeatStatus.UNAVAILABLE, updatedSeat.getStatus());
        verify(seatRepository).save(testSeat);
    }

    @Test
    void updateSeat_ShouldOnlyUpdateProvidedFields() {
        Long seatIdToUpdate = testSeat.getId();
        SeatRequest partialUpdateRequest = new SeatRequest();
        partialUpdateRequest.setSeatName("PartialUpdate");
        // hasSocket and status are null in partialUpdateRequest

        String originalSeatNumber = testSeat.getSeatNumber(); // Assuming seat number is not changed by seatName
        Boolean originalHasSocket = testSeat.isHasSocket(); // Corrected from getHasSocket
        Seat.SeatStatus originalStatus = testSeat.getStatus();


        when(seatRepository.findById(seatIdToUpdate)).thenReturn(Optional.of(testSeat));
        when(seatRepository.save(any(Seat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Seat updatedSeat = seatService.updateSeat(seatIdToUpdate, partialUpdateRequest);

        assertEquals("PartialUpdate", updatedSeat.getSeatName());
        assertEquals(originalSeatNumber, updatedSeat.getSeatNumber()); // Check if seatNumber remains if not updated
        assertEquals(originalHasSocket, updatedSeat.isHasSocket()); // Should remain original
        assertEquals(originalStatus, updatedSeat.getStatus()); // Should remain original
        verify(seatRepository).save(testSeat);
    }


    @Test
    void updateSeat_ShouldThrowException_WhenSeatNotExists() {
        Long seatIdToUpdate = 99L;
        SeatRequest updateRequest = new SeatRequest();
        updateRequest.setSeatName("AnyName");

        when(seatRepository.findById(seatIdToUpdate)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> seatService.updateSeat(seatIdToUpdate, updateRequest));
        assertEquals("Seat not found", exception.getMessage());
        verify(seatRepository, never()).save(any(Seat.class));
    }

    @Test
    void getSeats_ShouldReturnListOfSeats_ForGivenRoomId() {
        Long roomId = testRoom.getId();
        List<Seat> expectedSeats = Collections.singletonList(testSeat);
        when(seatRepository.findByRoomId(roomId)).thenReturn(expectedSeats);

        List<Seat> actualSeats = seatService.getSeats(roomId);

        assertNotNull(actualSeats);
        assertEquals(1, actualSeats.size());
        assertEquals(testSeat.getId(), actualSeats.get(0).getId());
        verify(seatRepository).findByRoomId(roomId);
    }
}