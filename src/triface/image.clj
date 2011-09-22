(ns triface.image
  (:use clojure.contrib.java-utils))

(import '(com.thebuzzmedia.imgscalr Scalr Scalr$Mode Scalr$Method))

(defn resize
  "resize the given image according to the supplied options, saving to the new filename"
  [filename new-filename opts]
  (let [img (javax.imageio.ImageIO/read (as-file filename))
        img-type (java.awt.image.BufferedImage/TYPE_INT_ARGB)
        aspect-ratio (/ (.getWidth img) (.getHeight img))
        subwidth (if (opts :width) (opts :width) (.getWidth img))
        subheight (if (opts :height) (opts :height) (.getHeight img))
        subratio (/ subwidth subheight)
        [mode dim] (if (> subratio aspect-ratio)
                     [Scalr$Mode/FIT_TO_HEIGHT subheight]
                     [Scalr$Mode/FIT_TO_WIDTH subwidth])
        scaled (Scalr/resize img Scalr$Method/QUALITY mode dim (into-array [Scalr/OP_ANTIALIAS]))]
    (javax.imageio.ImageIO/write scaled "png" (as-file (str new-filename ".png")))))
