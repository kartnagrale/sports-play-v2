package com.neml.badminton.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neml.badminton.entity.Championship;
import com.neml.badminton.entity.Team;
import com.neml.badminton.entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class MessageTrackerGateway {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public MessageTrackerGateway(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void queueChampionshipAdmin(User admin, String mobile, String temporaryPassword,
                                       Championship championship, String roomCode) {
        queueUser("CHAMPADMIN", "CHAMPIONSHIP_ADMIN_CREATED", admin, mobile, temporaryPassword,
                championship, roomCode, null,
                "Your PlayNeML championship administrator account");
    }

    public void queueTeamCaptain(User captain, String mobile, String temporaryPassword,
                                 Championship championship, Team team) {
        queueUser("TEAMCAPT", "TEAM_CAPTAIN_CREATED", captain, mobile, temporaryPassword,
                championship, championship.getRoomCode(), team,
                "Your PlayNeML team captain account");
    }

    private void queueUser(String transcode, String template, User user, String mobile,
                           String temporaryPassword, Championship championship, String roomCode,
                           Team team, String subject) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("template", template);
        parameters.put("championshipId", championship.getId());
        parameters.put("championshipName", championship.getName());
        parameters.put("roomCode", roomCode);
        if (team != null) {
            parameters.put("teamId", team.getId());
            parameters.put("teamName", team.getName());
        }
        parameters.put("userId", user.getId());
        parameters.put("fullName", user.getFullName());
        parameters.put("email", user.getEmail());
        parameters.put("mobileNumber", mobile);
        parameters.put("temporaryPassword", temporaryPassword);
        parameters.put("mustChangePassword", true);

        jdbc.update("""
                INSERT INTO play_neml.message_tracker (
                    msg_transcode, msg_message_type, msg_parameters, msg_mobile_nos,
                    msg_recipients_to, msg_subject, msg_status, msg_created_on,
                    msg_has_attachment, msg_owner, msg_request_id, msg_app_id,
                    app_id, db_type
                ) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 0, ?, ?, ?, ?, ?)
                """,
                transcode, "E", json(parameters), mobile, user.getEmail(), subject, "PENDING",
                "PLAY_NEML", user.getId().toString(), "SPORTS_PLAY",
                "PLAY_NEML", "POSTGRESQL");
    }

    private String json(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to prepare administrator notification", exception);
        }
    }
}
