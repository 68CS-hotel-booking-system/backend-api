package com.team.hotelbooking.booking.complaint.domain.model;

public record ComplaintCase(
    Complaint complaint,
    String bookingCode,
    String bookerName,
    String bookerEmail,
    String bookerPhone) {}
