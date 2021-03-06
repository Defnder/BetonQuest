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
package pl.betoncraft.betonquest.api;

import pl.betoncraft.betonquest.InstructionParseException;
import pl.betoncraft.betonquest.config.Config;
import pl.betoncraft.betonquest.config.ConfigPackage;

/**
 * Superclass for all conditions. You need to extend it in order to create new
 * custom conditions.
 * <p/>
 * Registering your condition is done through {@link
 * pl.betoncraft.betonquest.BetonQuest#registerConditions( String, Class<?
 * extends Condition>) registerConditions} method.
 * 
 * @author Jakub Sapalski
 */
abstract public class Condition {

    /**
     * Stores instruction string for the condition.
     */
    protected String instructions;
    /**
     * ConfigPackage in which this condition is defined
     */
    protected ConfigPackage pack;

    /**
     * Creates new instance of the condition. The condition should parse
     * instruction string at this point and extract all the data from it.
     * If anything goes wrong, throw {@link InstructionParseException}
     * with an error message describing the problem.
     * 
     * @param packName
     *            name of the package in which this condition is defined
     * @param instructions
     *            instruction string passed at runtime; you need to extract all
     *            required data from it and display errors if there is anything
     *            wrong.
     */
    public Condition(String packName, String instructions) throws InstructionParseException {
        this.instructions = instructions;
        this.pack = Config.getPackage(packName);
    }


    /**
     * This method should contain all logic for the condition and use data
     * parsed by the constructor. Don't worry about inverting the condition,
     * it's done by the rest of BetonQuest's logic. When this method is called
     * all the required data is present and parsed correctly.
     * 
     * @param playerID
     *          ID of the player for whom the condition will be checked
     * @return the result of the check
     */
    abstract public boolean check(String playerID);
}
