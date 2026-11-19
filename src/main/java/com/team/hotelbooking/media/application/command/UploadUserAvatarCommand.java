package com.team.hotelbooking.media.application.command;

import java.util.UUID;

public record UploadUserAvatarCommand(UUID actorId, String originalFilename, byte[] content) {}
