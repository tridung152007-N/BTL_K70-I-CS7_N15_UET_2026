package com.auction.server.model;

public class Vehicle extends Item {
    private String brand;
    private int kilometre;
    private int manufacturingYear;

    public Vehicle() { this.category = "VEHICLE"; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public int getKilometre() { return kilometre; }
    public void setKilometre(int kilometre) { this.kilometre = kilometre; }
    public int getManufacturingYear() { return manufacturingYear; }
    public void setManufacturingYear(int manufacturingYear) { this.manufacturingYear = manufacturingYear; }

    @Override
    public void printInfo() {
        System.out.println("[Vehicle] " + name + " | brand=" + brand + " (" + manufacturingYear + ")");
    }
}
