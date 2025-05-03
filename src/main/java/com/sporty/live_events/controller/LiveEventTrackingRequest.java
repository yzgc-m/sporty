package com.sporty.live_events.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/***
 Note:
 For demonstration purposes, I added an artificial constraint for the eventId below, as the task description asks
 for input validation, but there is not much to validate when long and boolean are chosen for the request field
 types - spring automatically handles most of the validation for these types.
 ***/
public record LiveEventTrackingRequest(
        @Min(1000) @Max(9999) long eventId,
        boolean status) {
}
