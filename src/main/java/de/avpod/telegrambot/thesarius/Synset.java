package de.avpod.telegrambot.thesarius;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.List;

/**
 * Created by apodznoev
 * date 15.08.2019.
 */
@Data
@Getter
@AllArgsConstructor
public class Synset {
    private long id;
    private List<Object> categories;
    private List<Term> terms;
    private List<List<Term>> supersynsets;
    private List<List<Term>> subsynsets;

}
