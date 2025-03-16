/*
 * Copyright (C) 2016 MCME
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
package com.mcmiddleearth.guidebook.util;

/**
 * @author Eriol_Eandur
 */
public class InputUtil {

    public static String replaceAltColorCode(String string) {
        return string.replace('#', '§');
    }

    public static String replaceColorCodeWithAltCode(String string) {
        return string.replace('§', '#');
    }

    public static String replaceBookColorCode(String string) {
        return replaceAltColorCode(string).replace("§0", "")
            .replace("§§", "§0")
            .replace("" + (char) 10, "\\n"); //string from book seem to contain random '§0' characters

    }
}
