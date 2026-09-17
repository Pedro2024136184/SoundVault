package com.soundvault.service;

import com.soundvault.util.AppLogger;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;

import java.io.File;

public class TaggingService {

    public boolean updateAlbumArtistTags(File file, String newAlbum, String newArtist) {
        if (file == null || !file.getName().toLowerCase().endsWith(".flac")) {
            return false;
        }
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTagOrCreateAndSetDefault();
            tag.setField(FieldKey.ALBUM, newAlbum);
            tag.setField(FieldKey.ARTIST, newArtist);
            audioFile.commit();
            return true;
        } catch (Exception e) {
            AppLogger.log("Erro ao atualizar tags de " + file.getName() + ": " + e.getMessage());
            return false;
        }
    }

    public boolean embedCoverArt(File file, byte[] imageData, String mimeType) {
        if (file == null || !file.getName().toLowerCase().endsWith(".flac")) {
            return false;
        }
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTagOrCreateAndSetDefault();
            Artwork artwork = ArtworkFactory.getNew();
            artwork.setBinaryData(imageData);
            artwork.setMimeType(mimeType != null ? mimeType : "image/jpeg");
            artwork.setPictureType(3); // Front Cover standard
            artwork.setDescription("Front Cover");

            tag.deleteArtworkField();
            tag.setField(artwork);
            audioFile.commit();
            return true;
        } catch (Exception e) {
            AppLogger.log("Erro ao embeber capa em " + file.getName() + ": " + e.getMessage());
            return false;
        }
    }

    public boolean hasCoverArt(File file) {
        if (file == null || !file.getName().toLowerCase().endsWith(".flac")) {
            return true;
        }
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();
            return tag != null && tag.getFirstArtwork() != null;
        } catch (Exception e) {
            return true; // Se estiver corrompido, assume true para não bloquear
        }
    }

    public boolean hasAlbumArtistTags(File file) {
        if (file == null || !file.getName().toLowerCase().endsWith(".flac")) {
            return true;
        }
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();
            if (tag == null) return false;
            String album = tag.getFirst(FieldKey.ALBUM);
            String artist = tag.getFirst(FieldKey.ARTIST);
            return album != null && !album.isBlank() && artist != null && !artist.isBlank();
        } catch (Exception e) {
            return true;
        }
    }
}
