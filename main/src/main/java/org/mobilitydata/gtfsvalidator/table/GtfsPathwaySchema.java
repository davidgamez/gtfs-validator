/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mobilitydata.gtfsvalidator.table;

import org.mobilitydata.gtfsvalidator.annotation.*;

@GtfsTable("pathways.txt")
public interface GtfsPathwaySchema extends GtfsEntity {
  @FieldType(FieldTypeEnum.ID)
  @PrimaryKey
  @RequiredValue
  String pathwayId();

  @FieldType(FieldTypeEnum.ID)
  @Index
  @RequiredValue
  @ForeignKey(table = "stops.txt", field = "stop_id")
  String fromStopId();

  @FieldType(FieldTypeEnum.ID)
  @Index
  @RequiredValue
  @ForeignKey(table = "stops.txt", field = "stop_id")
  String toStopId();

  @RequiredValue
  GtfsPathwayMode pathwayMode();

  @RequiredValue
  GtfsPathwayIsBidirectional isBidirectional();

  @NonNegative
  double length();

  @Positive
  int traversalTime();

  @NonZero
  int stairCount();

  double maxSlope();

  @Positive
  double minWidth();

  @MixedCase
  String signpostedAs();

  @MixedCase
  String reversedSignpostedAs();
}
