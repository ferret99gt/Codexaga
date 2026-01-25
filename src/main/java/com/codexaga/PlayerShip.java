package com.codexaga;

public final class PlayerShip
{
    private double x;
    private double y;
    private double invulnerableTimer;

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

    public void update(double delta)
    {
        if (invulnerableTimer > 0)
        {
            invulnerableTimer = Math.max(0, invulnerableTimer - delta);
        }
    }
}
