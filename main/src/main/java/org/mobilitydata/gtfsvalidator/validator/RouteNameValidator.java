/*
 * Copyright 2020 Google LLC, MobilityData IO
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

package org.mobilitydata.gtfsvalidator.validator;

import org.mobilitydata.gtfsvalidator.annotation.GtfsValidator;
import org.mobilitydata.gtfsvalidator.notice.NoticeContainer;
import org.mobilitydata.gtfsvalidator.notice.SeverityLevel;
import org.mobilitydata.gtfsvalidator.notice.ValidationNotice;
import org.mobilitydata.gtfsvalidator.table.GtfsRoute;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates short and long name for a single route.
 *
 * <p>References:
 *
 * <ul>
 *   <li>&lt;a href="https://gtfs.org/schedule/reference/#routestxt")"&gt;GTFS routes.txt
 *       specification&lt;/a&gt;
 *   <li>&lt;a href="https://gtfs.org/schedule/best-practices/#routestxt"&gt;GTFS routes.txt Best
 *       Practices&lt;/a&gt;
 * </ul>
 *
 * <p>Generated notices:
 *
 * <ul>
 *   <li>{@link RouteBothShortAndLongNameMissingNotice}
 *   <li>{@link RouteShortAndLongNameEqualNotice}
 *   <li>{@link RouteShortNameTooLongNotice}
 *   <li>{@link SameNameAndDescriptionForRouteNotice}
 *   <li>{@link RouteLongNameContainsShortNameNotice}
 * </ul>
 */
@GtfsValidator
public class RouteNameValidator extends SingleEntityValidator<GtfsRoute> {
  private static final int MAX_SHORT_NAME_LENGTH = 12;

  @Override
  public void validate(GtfsRoute entity, NoticeContainer noticeContainer) {
    final boolean hasLongName = entity.hasRouteLongName();
    final boolean hasShortName = entity.hasRouteShortName();

    if (!hasLongName && !hasShortName) {
      noticeContainer.addValidationNotice(
          new RouteBothShortAndLongNameMissingNotice(entity.routeId(), entity.csvRowNumber()));
    }

    if (hasShortName
        && hasLongName
        && entity.routeShortName().equalsIgnoreCase(entity.routeLongName())) {
      noticeContainer.addValidationNotice(
          new RouteShortAndLongNameEqualNotice(
              entity.routeId(), entity.csvRowNumber(),
              entity.routeShortName(), entity.routeLongName()));
    }

    if (hasShortName && entity.routeShortName().length() > MAX_SHORT_NAME_LENGTH) {
      noticeContainer.addValidationNotice(
          new RouteShortNameTooLongNotice(
              entity.routeId(), entity.csvRowNumber(), entity.routeShortName()));
    }

    // check if route_long_name begins with route_short_name followed by " ", "-", or "(".
    // as referenced here
    // https://github.com/MobilityData/gtfs-validator/pull/501#discussion_r535506016
    if (hasLongName && hasShortName) {
      String longName = entity.routeLongName();
      String shortName = entity.routeShortName();
      Pattern pattern = Pattern.compile("^" + shortName + "[\\s\\-\\(]");
      Matcher matcher = pattern.matcher(longName);
      if(matcher.find()) {
        noticeContainer.addValidationNotice(
            new RouteLongNameContainsShortNameNotice(
                entity.routeId(), entity.csvRowNumber(), shortName, longName));
      }
    }

    if (entity.hasRouteDesc()) {
      String routeDesc = entity.routeDesc();
      String routeId = entity.routeId();
      if (hasShortName && !isValidRouteDesc(routeDesc, entity.routeShortName())) {
        noticeContainer.addValidationNotice(
            new SameNameAndDescriptionForRouteNotice(
                entity.csvRowNumber(), routeId, routeDesc, "route_short_name"));
        return;
      }
      if (hasLongName && !isValidRouteDesc(routeDesc, entity.routeLongName())) {
        noticeContainer.addValidationNotice(
            new SameNameAndDescriptionForRouteNotice(
                entity.csvRowNumber(), routeId, routeDesc, "route_long_name"));
      }
    }
  }

  private boolean isValidRouteDesc(String routeDesc, String routeShortOrLongName) {
    // ignore lower case and upper case difference
    return !routeDesc.equalsIgnoreCase(routeShortOrLongName);
  }

  /**
   * Both `routes.route_short_name` and `routes.route_long_name` are missing for a route.
   *
   * <p>Severity: {@code SeverityLevel.ERROR}
   */
  static class RouteBothShortAndLongNameMissingNotice extends ValidationNotice {
    private final String routeId;
    private final int csvRowNumber;

    RouteBothShortAndLongNameMissingNotice(String routeId, int csvRowNumber) {
      super(SeverityLevel.ERROR);
      this.routeId = routeId;
      this.csvRowNumber = csvRowNumber;
    }
  }

  /**
   * Long name can not contain short name for a single route.
   *
   * <p>Severity: {@code SeverityLevel.WARNING}
   */
  static class RouteLongNameContainsShortNameNotice extends ValidationNotice {
    private final String routeId;
    private final int csvRowNumber;
    private final String routeShortName;
    private final String routeLongName;

    RouteLongNameContainsShortNameNotice(
        String routeId, int csvRowNumber, String routeShortName, String routeLongName) {
      super(SeverityLevel.WARNING);
      this.routeId = routeId;
      this.csvRowNumber = csvRowNumber;
      this.routeShortName = routeShortName;
      this.routeLongName = routeLongName;
    }
  }

  /**
   * Short and long name are equal for a single route.
   *
   * <p>Severity: {@code SeverityLevel.WARNING}
   */
  static class RouteShortAndLongNameEqualNotice extends ValidationNotice {
    private final String routeId;
    private final int csvRowNumber;
    private final String routeShortName;
    private final String routeLongName;

    RouteShortAndLongNameEqualNotice(
        String routeId, int csvRowNumber, String routeShortName, String routeLongName) {
      super(SeverityLevel.WARNING);
      this.routeId = routeId;
      this.csvRowNumber = csvRowNumber;
      this.routeShortName = routeShortName;
      this.routeLongName = routeLongName;
    }
  }

  /**
   * Short name of a single route is too long (more than 12 characters,
   * https://gtfs.org/best-practices/#routestxt).
   *
   * <p>Severity: {@code SeverityLevel.WARNING}
   */
  static class RouteShortNameTooLongNotice extends ValidationNotice {
    private final String routeId;
    private final int csvRowNumber;
    private final String routeShortName;

    RouteShortNameTooLongNotice(String routeId, int csvRowNumber, String routeShortName) {
      super(SeverityLevel.WARNING);
      this.routeId = routeId;
      this.csvRowNumber = csvRowNumber;
      this.routeShortName = routeShortName;
    }
  }

  /**
   * A single route has identical values for {@code routes.route_desc} and {@code route_long_name}
   * or {@code route_short_name}.
   *
   * <p>Severity: {@code SeverityLevel.WARNING}
   */
  static class SameNameAndDescriptionForRouteNotice extends ValidationNotice {
    private final int csvRowNumber;
    private final String routeId;
    private final String routeDesc;
    private final String specifiedField;

    SameNameAndDescriptionForRouteNotice(
        int csvRowNumber, String routeId, String routeDesc, String routeShortOrLongName) {
      super(SeverityLevel.WARNING);
      this.routeId = routeId;
      this.csvRowNumber = csvRowNumber;
      this.routeDesc = routeDesc;
      this.specifiedField = routeShortOrLongName;
    }
  }
}
