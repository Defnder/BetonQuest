/**
 * BetonQuest - advanced quests for Bukkit
 * Copyright (C) 2015  Jakub "Co0sh" Sapalski
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package pl.betoncraft.betonquest.conditions;

import pl.betoncraft.betonquest.BetonQuest;
import pl.betoncraft.betonquest.InstructionParseException;
import pl.betoncraft.betonquest.api.Condition;

/**
 * All of specified conditions have to be true
 * 
 * @author Jakub Sapalski
 */
public class ConjunctionCondition extends Condition {

    private final String[] conditions;

    public ConjunctionCondition(String packName, String instructions)
            throws InstructionParseException {
        super(packName, instructions);
        String[] parts = instructions.split(" ");
        if (parts.length < 2) {
            throw new InstructionParseException("Conditions not defined");
        }
        String[] tempConditions = parts[1].split(",");
        for (int i = 0; i < tempConditions.length; i++) {
            if (!tempConditions[i].contains(".")) {
                tempConditions[i] = packName + "." + tempConditions[i];
            }
        }
        conditions = tempConditions;
    }

    @Override
    public boolean check(String playerID) {
        for (String condition : conditions) {
            if (!BetonQuest.condition(playerID, condition)) {
                return false;
            }
        }
        return true;
    }
}
