(ns caribou.template.adapter
  (import java.io.File)
  (import com.instrument.caribou.template.CaribouTemplateEngine))
  
  (def ^:dynamic *caribou-template-engine* (ref {}))

  (defn init-caribou-template-engine [base-template-directory]
    (dosync (ref-set *caribou-template-engine* (CaribouTemplateEngine. base-template-directory))))

  (defn get-renderer [template]
    (fn [objectmap]
      (.render (deref *caribou-template-engine*) objectmap template)))