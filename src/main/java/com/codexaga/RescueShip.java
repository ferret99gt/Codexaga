package com.codexaga;

public final class RescueShip
{
    private double x;
    private double y;
    private final double baseX;
    private final double speed;
    private double timer;

    public RescueShip(double x, double y, double speed)
    {
        this.x = x;
        this.y = y;
        this.baseX = x;
        this.speed = speed;
    }

    public void update(double delta)
    {
        timer += delta;
        y += speed * delta;
        x = baseX + Math.sin(timer * 3.6) * 34;
    }

    public double getX()
    {
        return x;
    }

    public double getY()
    {
        return y;
    }
}
