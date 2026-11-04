package com.team.hotelbooking.identity.application.usecase;

import com.team.hotelbooking.identity.application.result.RoleResult;
import java.util.List;
import java.util.UUID;

public interface ListRolesUseCase {

  List<RoleResult> execute(UUID actorId);
}
