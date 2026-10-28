package com.neml.badminton.config;
import com.neml.badminton.service.*;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.Path;
@Configuration
public class PlayerPhotoStorageConfig {
 @Bean @ConditionalOnMissingBean(PlayerPhotoStorage.class)
 PlayerPhotoStorage playerPhotoStorage(@Value("${app.players.photo.directory:./var/player-photos}") String directory){return new LocalPlayerPhotoStorage(Path.of(directory));}
}
