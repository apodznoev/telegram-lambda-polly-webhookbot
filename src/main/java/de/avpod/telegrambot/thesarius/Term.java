package de.avpod.telegrambot.thesarius;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * Created by apodznoev
 * date 15.08.2019.
 */
@Data
@Getter
@AllArgsConstructor
public class Term {
    private String term;
    private String level;

    @Override
    public String toString() {
        return term + (level != null ? "(" + level + ")" : "");
    }
}
