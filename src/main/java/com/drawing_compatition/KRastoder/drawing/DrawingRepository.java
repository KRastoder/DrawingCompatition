package com.drawing_compatition.KRastoder.drawing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DrawingRepository extends JpaRepository<Drawing, Long> {

    @Query("""
            select new com.drawing_compatition.KRastoder.drawing.DrawingSummary(
                d.id,
                d.description,
                d.createdAt,
                u.id,
                u.username,
                u.displayName,
                count(v.id),
                sum(case when v.user.id = :viewerId then 1 else 0 end)
            )
            from Drawing d
            join d.user u
            left join d.votes v
            where d.createdAt >= :from
              and d.createdAt < :to
            group by d.id, d.description, d.createdAt, u.id, u.username, u.displayName
            order by count(v.id) desc, d.createdAt desc, d.id desc
            """)
    List<DrawingSummary> findSummaries(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("viewerId") Long viewerId
    );

    @Query("""
            select new com.drawing_compatition.KRastoder.drawing.DrawingSummary(
                d.id,
                d.description,
                d.createdAt,
                u.id,
                u.username,
                u.displayName,
                count(v.id),
                sum(case when v.user.id = :viewerId then 1 else 0 end)
            )
            from Drawing d
            join d.user u
            left join d.votes v
            where d.user.id = :ownerId
            group by d.id, d.description, d.createdAt, u.id, u.username, u.displayName
            order by d.createdAt desc, d.id desc
            """)
    List<DrawingSummary> findSummariesByOwner(
            @Param("ownerId") Long ownerId,
            @Param("viewerId") Long viewerId
    );

    @Query("""
            select new com.drawing_compatition.KRastoder.drawing.DrawingSummary(
                d.id,
                d.description,
                d.createdAt,
                u.id,
                u.username,
                u.displayName,
                count(v.id),
                sum(case when v.user.id = :viewerId then 1 else 0 end)
            )
            from Drawing d
            join d.user u
            left join d.votes v
            where d.id = :id
            group by d.id, d.description, d.createdAt, u.id, u.username, u.displayName
            """)
    Optional<DrawingSummary> findSummaryById(@Param("id") Long id, @Param("viewerId") Long viewerId);

    @Query("""
            select new com.drawing_compatition.KRastoder.drawing.DrawingImage(d.imageData, d.contentType)
            from Drawing d
            where d.id = :id
            """)
    Optional<DrawingImage> findImageById(@Param("id") Long id);

    @Query("select d.user.id from Drawing d where d.id = :id")
    Optional<Long> findOwnerIdById(@Param("id") Long id);
}
