package com.drawing_compatition.KRastoder.vote;

import com.drawing_compatition.KRastoder.auth.AppUserPrincipal;
import com.drawing_compatition.KRastoder.drawing.DrawingResponse;
import com.drawing_compatition.KRastoder.drawing.DrawingService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/drawings/{drawingId}/votes")
public class VoteController {

    private final VoteService voteService;
    private final DrawingService drawingService;

    public VoteController(VoteService voteService, DrawingService drawingService) {
        this.voteService = voteService;
        this.drawingService = drawingService;
    }

    @PostMapping
    public DrawingResponse vote(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long drawingId
    ) {
        voteService.vote(drawingId, principal.id());
        return drawingService.get(drawingId, principal.id());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unvote(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long drawingId
    ) {
        voteService.unvote(drawingId, principal.id());
    }
}
