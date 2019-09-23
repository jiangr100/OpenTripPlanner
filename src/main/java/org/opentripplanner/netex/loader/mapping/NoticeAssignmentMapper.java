package org.opentripplanner.netex.loader.mapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.loader.util.ReadOnlyHierarchicalMap;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import static org.opentripplanner.netex.loader.mapping.FeedScopedIdFactory.createFeedScopedId;

/**
 * Maps NeTEx NoticeAssignment, which is the connection between a Notice and the object it refers
 * to. In the case of a notice referring to a StopPointInJourneyPattern, which has no OTP equivalent,
 * it will be assigned to its corresponding TimeTabledPassingTimes for each ServiceJourney in the
 * same JourneyPattern.
 *
 * In order to maintain this connection to TimeTabledPassingTime (StopTime in OTP), it is necessary
 * to assign the TimeTabledPassingTime id to its corresponding StopTime.
 */
class NoticeAssignmentMapper {

    private final ReadOnlyHierarchicalMap<String, Collection<TimetabledPassingTime>> passingTimeByStopPointId;

    private final ReadOnlyHierarchicalMap<String, org.rutebanken.netex.model.Notice> noticesById;

    private final EntityById<FeedScopedId, Route> routesById;

    private final Map<String, StopTime> stopTimesByNetexId;

    private final EntityById<FeedScopedId, Trip> tripsById;

    /** Note! The notce mapper cashes notices, making sure duplicates are not created. */
    private final NoticeMapper noticeMapper = new NoticeMapper();

    private static final Logger LOG = LoggerFactory.getLogger(NoticeAssignmentMapper.class);

    NoticeAssignmentMapper(
            ReadOnlyHierarchicalMap<String, Collection<TimetabledPassingTime>> passingTimeByStopPointId,
            ReadOnlyHierarchicalMap<String, org.rutebanken.netex.model.Notice> noticesById,
            EntityById<FeedScopedId, Route> routesById,
            Map<String, StopTime> stopTimesByNetexId,
            EntityById<FeedScopedId, Trip> tripsById
    ) {
        this.passingTimeByStopPointId = passingTimeByStopPointId;
        this.noticesById = noticesById;
        this.routesById = routesById;
        this.stopTimesByNetexId = stopTimesByNetexId;
        this.tripsById = tripsById;
    }

    Multimap<Serializable, Notice> map(NoticeAssignment noticeAssignment){
        Multimap<Serializable, Notice> noticeByElement = ArrayListMultimap.create();

        String noticedObjectId = noticeAssignment.getNoticedObjectRef().getRef();
        Notice otpNotice = getOrMapNotice(noticeAssignment);

        if(otpNotice == null) {
            LOG.warn("Notice in notice assignment is missing for assignment {}", noticeAssignment);
            return noticeByElement;
        }

        Collection<TimetabledPassingTime> times =  passingTimeByStopPointId.lookup(noticedObjectId);

        // Special case for StopPointInJourneyPattern
        if (!times.isEmpty()) {
            for (TimetabledPassingTime time : times) {
                noticeByElement.put(
                        stopTimesByNetexId.get(time.getId()).getId(),
                        otpNotice
                );
            }
        } else {
            FeedScopedId otpId = createFeedScopedId(noticedObjectId);

            if (stopTimesByNetexId.containsKey(noticedObjectId)) {
                noticeByElement.put(otpId, otpNotice);
            } else if (routesById.containsKey(otpId)) {
                noticeByElement.put(otpId, otpNotice);
            } else if (tripsById.containsKey(otpId)) {
                noticeByElement.put(otpId, otpNotice);
            } else {
                LOG.warn("Could not map noticeAssignment for element with id {}", noticedObjectId);
            }
        }
        return noticeByElement;
    }

    private Notice getOrMapNotice(NoticeAssignment assignment) {
        org.rutebanken.netex.model.Notice notice = assignment.getNotice() != null
                ? assignment.getNotice()
                : noticesById.lookup(assignment.getNoticeRef().getRef());

        return notice == null ? null : noticeMapper.map(notice);
    }
}