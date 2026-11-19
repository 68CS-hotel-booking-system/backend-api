package com.team.hotelbooking.inventory.application.usecase;

import com.team.hotelbooking.inventory.application.command.ListRoomBlocksQuery;
import com.team.hotelbooking.inventory.application.result.RoomBlockResult;
import java.util.List;

public interface ListRoomBlocksUseCase {

  List<RoomBlockResult> execute(ListRoomBlocksQuery query);
}
