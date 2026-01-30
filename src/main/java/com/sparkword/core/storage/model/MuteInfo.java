/*
 * This file is part of SparkWord - https://github.com/SparkCry/SparkWord
 * Copyright (C) 2026 SparkCry and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.sparkword.core.storage.model;

public record MuteInfo(boolean isMuted, String staff, String reason, long expiry, MuteScope scope) {

    public static final MuteInfo NOT_MUTED = new MuteInfo(false, null, null, 0, MuteScope.CHAT);

    public boolean hasExpired() {
        return isMuted && expiry != 0 && System.currentTimeMillis() > expiry;
    }

    public boolean isPermanent() {
        return isMuted && expiry == 0;
    }

    public boolean blocks(MuteScope requiredScope) {
        if (!isMuted) return false;
        if (hasExpired()) return false;

        if (this.scope == MuteScope.GLOBAL) return true;

        return requiredScope == MuteScope.CHAT;
    }

    public enum MuteScope {
        CHAT,
        GLOBAL
    }
}
