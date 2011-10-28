(ns triface.template.adapter
  (import java.io.File)
  (import com.instrument.triface.template.TrifaceTemplateEngine))
  
  (def ^:dynamic *triface-template-engine* (ref {}))

  (defn init-triface-template-engine [base-template-directory]
    (dosync (ref-set *triface-template-engine* (TrifaceTemplateEngine. base-template-directory))))

  (defn get-renderer [template]
    (fn [objectmap]
      (.render (deref *triface-template-engine*) objectmap template)))