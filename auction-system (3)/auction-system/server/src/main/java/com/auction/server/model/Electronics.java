package com.auction.server.model;

public class Electronics extends Item {
    private String brand;
    private String warrantyMonths;

    public Electronics() { this.category = "ELECTRONICS"; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(String warrantyMonths) { this.warrantyMonths = warrantyMonths; }

    @Override
    public void printInfo() {
        System.out.println("[Electronics] " + name + " | brand=" + brand);
    }
}
