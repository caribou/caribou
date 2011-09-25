(ns triface.image
  (:use rinzelight.image)
  (:use rinzelight.rendering-hints)
  (:use rinzelight.effects.affine-transforms))

(defn resize
  "resize the image specified by filename according to the supplied options
  (:width or :height), saving to file new-filename"
  [filename new-filename opts]
  (let [img (read-image filename)
        width (img :width)
        height (img :height)
        aspect-ratio (/ width height)
        subwidth (if (opts :width) (opts :width) width)
        subheight (if (opts :height) (opts :height) height)
        subratio (/ subwidth subheight)
        master (if (> subratio aspect-ratio)
                 (/ subheight height)
                 (/ subwidth width))
        scaled (scale img master antialiasing-on interpolation-bicubic)]
    (write-image scaled new-filename)))

