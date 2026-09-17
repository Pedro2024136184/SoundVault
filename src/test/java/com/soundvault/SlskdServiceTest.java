package com.soundvault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.soundvault.model.PeerResult;
import com.soundvault.service.SlskdService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SlskdServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SlskdService slskdService = new SlskdService();

    @Test
    void testSelectBestPeerPrioritizesFlacAndFreeSlots() {
        ObjectNode peer1 = mapper.createObjectNode();
        peer1.put("username", "user_mp3_fast");
        peer1.put("uploadSpeed", 5000000.0);
        peer1.put("hasFreeUploadSlots", true);
        ArrayNode files1 = peer1.putArray("files");
        ObjectNode f1 = files1.addObject();
        f1.put("filename", "C:\\Music\\Album\\track.mp3");
        f1.put("size", 10000000);

        ObjectNode peer2 = mapper.createObjectNode();
        peer2.put("username", "user_flac_good");
        peer2.put("uploadSpeed", 1000000.0);
        peer2.put("hasFreeUploadSlots", true);
        ArrayNode files2 = peer2.putArray("files");
        ObjectNode f2 = files2.addObject();
        f2.put("filename", "C:\\Music\\Album\\track.flac");
        f2.put("size", 40000000);

        PeerResult best = slskdService.selectBestPeer(List.of(peer1, peer2), 0);

        assertNotNull(best);
        assertEquals("user_flac_good", best.username());
        assertEquals(1, best.getTrackCount());
        assertTrue(best.hasFreeSlots());
    }

    @Test
    void testSelectBestPeerMatchesExpectedTrackCount() {
        // Peer with only 2 tracks
        ObjectNode peer1 = mapper.createObjectNode();
        peer1.put("username", "incomplete_album_peer");
        peer1.put("uploadSpeed", 5000000.0);
        peer1.put("hasFreeUploadSlots", true);
        ArrayNode files1 = peer1.putArray("files");
        for (int i = 1; i <= 2; i++) {
            ObjectNode f = files1.addObject();
            f.put("filename", "C:\\Music\\Album\\track" + i + ".flac");
            f.put("size", 30000000);
        }

        // Peer with full 10 tracks
        ObjectNode peer2 = mapper.createObjectNode();
        peer2.put("username", "full_album_peer");
        peer2.put("uploadSpeed", 2000000.0);
        peer2.put("hasFreeUploadSlots", true);
        ArrayNode files2 = peer2.putArray("files");
        for (int i = 1; i <= 10; i++) {
            ObjectNode f = files2.addObject();
            f.put("filename", "C:\\Music\\Album\\track" + i + ".flac");
            f.put("size", 30000000);
        }

        // Demanding 10 tracks
        PeerResult best = slskdService.selectBestPeer(List.of(peer1, peer2), 10);

        assertNotNull(best);
        assertEquals("full_album_peer", best.username());
        assertEquals(10, best.getTrackCount());
    }
}
