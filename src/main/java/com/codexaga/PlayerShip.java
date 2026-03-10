package com.codexaga;

public final class PlayerShip
{
    private double x;
    private final double y;
    private double invulnerableTimer;
    private boolean dualFighter;

    public PlayerShip(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    public double getX()
    {
        return x;
    }

    public void setX(double x)
    {
        this.x = x;
    }

    public double getY()
    {
        return y;
    }

    public double getInvulnerableTimer()
    {
        return invulnerableTimer;
    }

    public void setInvulnerable(double seconds)
    {
        invulnerableTimer = seconds;
    }

    public boolean isInvulnerable()
    {
        return invulnerableTimer > 0;
    }

    public boolean isDualFighter()
    {
        return dualFighter;
    }

    public void setDualFighter(boolean dualFighter)
    {
        this.dualFighter = dualFighter;
    }

    public double getCollisionHalfWidth()
    {
        return (dualFighter ? GameConfig.DUAL_PLAYER_WIDTH : GameConfig.PLAYER_WIDTH) * 0.5;
    }

    public void update(double delta)
    {
        if (invulnerableTimer > 0)
        {
            invulnerableTimer = Math.max(0, invulnerableTimer - delta);
        }
    }
}
