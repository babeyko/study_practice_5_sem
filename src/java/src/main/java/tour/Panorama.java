package tour;

abstract sealed class Panorama permits Panorama.Hinted, Panorama.FromFile {
    private final Long panoramaId;
    private final Long propertyId;
    private final String fileName;

    private Panorama(Long panoramaId, Long propertyId, String fileName) {
        this.panoramaId = panoramaId;
        this.propertyId = propertyId;
        this.fileName = fileName;
    }

    static Panorama create(Long panoramaId, Long propertyId, String fileName, String roomHint) {
        if (roomHint != null && !roomHint.isEmpty()) {
            return new Hinted(panoramaId, propertyId, fileName, roomHint);
        }
        return new FromFile(panoramaId, propertyId, fileName);
    }

    Long panoramaId() {
        return panoramaId;
    }

    Long propertyId() {
        return propertyId;
    }

    String fileName() {
        return fileName;
    }

    abstract String sceneName();

    static final class Hinted extends Panorama {
        private final String roomHint;

        private Hinted(Long panoramaId, Long propertyId, String fileName, String roomHint) {
            super(panoramaId, propertyId, fileName);
            this.roomHint = roomHint;
        }

        @Override
        String sceneName() {
            return roomHint;
        }
    }

    static final class FromFile extends Panorama {
        private FromFile(Long panoramaId, Long propertyId, String fileName) {
            super(panoramaId, propertyId, fileName);
        }

        @Override
        String sceneName() {
            return fileName();
        }
    }
}
