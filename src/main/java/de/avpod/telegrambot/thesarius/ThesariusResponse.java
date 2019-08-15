package de.avpod.telegrambot.thesarius;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Created by apodznoev
 * date 15.08.2019.
 */
@Data
@AllArgsConstructor
public class ThesariusResponse {
    private Map<String, Object> metaData;
    private List<Synset> synsets;

}
