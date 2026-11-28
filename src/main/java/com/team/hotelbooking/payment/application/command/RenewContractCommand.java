package com.team.hotelbooking.payment.application.command;

import java.util.UUID;

public record RenewContractCommand(UUID previousContractId, CreateContractCommand replacement) {}
