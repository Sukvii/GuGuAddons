package com.gugucraft.guguaddons.stock;

import java.util.List;

public final class StockCatalog {
    private static final List<StockDefinition> STOCKS = List.of(
            new StockDefinition("AUR", "Aurora Engines", 720, 0.00008, 0.0095, 1.20),
            new StockDefinition("BRS", "Brassline Transit", 540, 0.00005, 0.0085, 1.10),
            new StockDefinition("CRN", "Crown Dynamics", 1480, 0.00010, 0.0100, 1.35),
            new StockDefinition("DCT", "DeductTech Labs", 1260, 0.00007, 0.0110, 1.25),
            new StockDefinition("ECH", "Echo Storage", 610, 0.00004, 0.0075, 0.95),
            new StockDefinition("FLX", "Flux Metallurgy", 980, 0.00006, 0.0105, 1.30),
            new StockDefinition("GMA", "Gamma Freight", 820, 0.00004, 0.0080, 1.05),
            new StockDefinition("HZN", "Horizon Utilities", 460, 0.00003, 0.0062, 0.70),
            new StockDefinition("IRN", "Ironpoint Works", 670, 0.00002, 0.0090, 1.15),
            new StockDefinition("JDE", "Jade Networks", 890, 0.00005, 0.0092, 1.05),

            new StockDefinition("KNT", "Kinetic Systems", 1100, 0.00009, 0.0115, 1.40),
            new StockDefinition("LMN", "Lumen Optics", 760, 0.00006, 0.0088, 1.00),
            new StockDefinition("MTR", "Meteor Rail", 1320, 0.00007, 0.0102, 1.25),
            new StockDefinition("NVA", "Nova Foods", 510, 0.00003, 0.0068, 0.80),
            new StockDefinition("OBS", "Obsidian Logistics", 930, 0.00004, 0.0096, 1.18),
            new StockDefinition("PRM", "Prism Textiles", 590, 0.00003, 0.0074, 0.88),
            new StockDefinition("QRT", "Quartz Robotics", 1680, 0.00011, 0.0125, 1.50),
            new StockDefinition("RVT", "Rivet Construction", 840, 0.00004, 0.0091, 1.08),
            new StockDefinition("SLR", "Solar Crest Energy", 960, 0.00008, 0.0100, 1.22),
            new StockDefinition("TRN", "Trenchwater Shipping", 560, 0.00001, 0.0086, 1.12),

            new StockDefinition("UMA", "Umbral Security", 1020, 0.00005, 0.0108, 1.28),
            new StockDefinition("VLT", "Volt Forge", 1370, 0.00009, 0.0120, 1.45),
            new StockDefinition("WDN", "Warden Mining", 780, 0.00002, 0.0098, 1.20),
            new StockDefinition("XEN", "Xeno Biotech", 1840, 0.00012, 0.0130, 1.52),
            new StockDefinition("YRD", "Yardline Motors", 1160, 0.00007, 0.0112, 1.33),
            new StockDefinition("ZTH", "Zenith Housing", 640, 0.00003, 0.0078, 0.92),
            new StockDefinition("ALP", "Alpine Coldchain", 730, 0.00004, 0.0082, 0.96),
            new StockDefinition("BLD", "Boulder Finance", 1230, 0.00005, 0.0101, 1.12),
            new StockDefinition("CST", "Coastline Telecom", 940, 0.00006, 0.0094, 1.07),
            new StockDefinition("DRY", "Drydock Repairs", 620, 0.00002, 0.0087, 1.10),

            new StockDefinition("ELM", "Elmfield Retail", 500, 0.00003, 0.0071, 0.85),
            new StockDefinition("FRN", "Frontier Farming", 420, 0.00002, 0.0065, 0.74),
            new StockDefinition("GLD", "Goldline Securities", 1520, 0.00009, 0.0116, 1.38),
            new StockDefinition("HLD", "Heliodor Digital", 1280, 0.00008, 0.0119, 1.41),
            new StockDefinition("ITR", "Itera Components", 870, 0.00004, 0.0093, 1.04),
            new StockDefinition("JST", "Jetstream Cargo", 980, 0.00005, 0.0104, 1.24),
            new StockDefinition("KRL", "Kestrel Aviation", 1410, 0.00007, 0.0122, 1.47),
            new StockDefinition("LCK", "Lockline Banking", 1190, 0.00003, 0.0089, 0.90),
            new StockDefinition("MNT", "Mintel Electronics", 1330, 0.00008, 0.0113, 1.30),
            new StockDefinition("NRV", "Nerva Pharma", 1710, 0.00010, 0.0128, 1.46),

            new StockDefinition("OPT", "Opticore Media", 860, 0.00006, 0.0097, 1.09),
            new StockDefinition("PLM", "Palmetto Chemicals", 690, 0.00004, 0.0090, 1.16),
            new StockDefinition("QBL", "Quibble Games", 1120, 0.00007, 0.0107, 1.31),
            new StockDefinition("RDN", "Redstone Fabrics", 760, 0.00003, 0.0084, 0.98),
            new StockDefinition("SNT", "Sentinel Defense", 1590, 0.00006, 0.0121, 1.44),
            new StockDefinition("TLK", "Talloak Timber", 480, 0.00001, 0.0067, 0.72),
            new StockDefinition("URB", "Urban Habitat", 650, 0.00003, 0.0081, 0.93),
            new StockDefinition("VSN", "Vision Arcades", 920, 0.00005, 0.0106, 1.27),
            new StockDefinition("WLF", "Wolfpack Security", 1040, 0.00004, 0.0099, 1.15),
            new StockDefinition("XPL", "Xplore Materials", 1210, 0.00008, 0.0111, 1.29));

    static {
        if (STOCKS.size() != 50) {
            throw new IllegalStateException("Stock catalog must contain exactly 50 stocks.");
        }
    }

    private StockCatalog() {
    }

    public static int size() {
        return STOCKS.size();
    }

    public static StockDefinition get(int index) {
        return STOCKS.get(index);
    }
}
