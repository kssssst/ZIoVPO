package com.example.market.dto.license;

public class TicketResponse {

    private Ticket ticket;
    private String signature;

    public TicketResponse(Ticket ticket) {
        this.ticket = ticket;
        this.signature = null;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public String getSignature() {
        return signature;
    }
}