public class AuctionManager {
    // Bước 1: Tạo biến static duy nhất của lớp
    private static AuctionManager instance;
    private List<Auction> auctions;

    // Bước 2: Để Constructor là PRIVATE để ngăn chặn việc tạo đối tượng từ bên ngoài bằng "new"
    private AuctionManager() {
        auctions = new ArrayList<>();
    }

    // Bước 3: Tạo phương thức static để lấy đối tượng duy nhất
    public static AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public void addAuction(Auction a) {
        auctions.add(a);
    }
}