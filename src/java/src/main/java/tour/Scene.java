package tour;

record Scene(Long sceneId, Long tourId, Long roomId, Long panoramaId,
             String name, Integer orderIndex, Boolean isStart) {
}
