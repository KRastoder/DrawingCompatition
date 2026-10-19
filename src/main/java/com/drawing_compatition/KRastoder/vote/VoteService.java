package com.drawing_compatition.KRastoder.vote;

import com.drawing_compatition.KRastoder.common.ConflictException;
import com.drawing_compatition.KRastoder.common.NotFoundException;
import com.drawing_compatition.KRastoder.drawing.Drawing;
import com.drawing_compatition.KRastoder.drawing.DrawingRepository;
import com.drawing_compatition.KRastoder.user.User;
import com.drawing_compatition.KRastoder.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoteService {

    private final VoteRepository votes;
    private final DrawingRepository drawings;
    private final UserRepository users;

    public VoteService(VoteRepository votes, DrawingRepository drawings, UserRepository users) {
        this.votes = votes;
        this.drawings = drawings;
        this.users = users;
    }

    @Transactional
    public void vote(Long drawingId, Long userId) {
        if (!drawings.existsById(drawingId)) {
            throw new NotFoundException("Drawing not found.");
        }

        if (votes.existsByDrawingIdAndUserId(drawingId, userId)) {
            throw new ConflictException("You have already voted for this drawing.");
        }

        Drawing drawing = drawings.getReferenceById(drawingId);
        User user = users.getReferenceById(userId);

        try {
            votes.saveAndFlush(new Vote(drawing, user));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("You have already voted for this drawing.");
        }
    }

    @Transactional
    public void unvote(Long drawingId, Long userId) {
        if (!drawings.existsById(drawingId)) {
            throw new NotFoundException("Drawing not found.");
        }

        Vote vote = votes.findByDrawingIdAndUserId(drawingId, userId)
                .orElseThrow(() -> new NotFoundException("You have not voted for this drawing."));

        votes.delete(vote);
    }
}
