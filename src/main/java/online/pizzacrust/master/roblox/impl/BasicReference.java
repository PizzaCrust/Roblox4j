package online.pizzacrust.master.roblox.impl;

import online.pizzacrust.master.roblox.Robloxian;
import online.pizzacrust.master.roblox.errors.InvalidUserException;

public class BasicReference implements Robloxian.LightReference {

    private final int userId;
    private final String username;

    public BasicReference(int userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    @Override
    public Robloxian grab() throws InvalidUserException {
        return new BasicRobloxian(getUsername());
    }

    @Override
    public int getUserId() {
        return this.userId;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

}