package main

// centralized http repo for face recognition trainer file

import (
	"bytes"
	"compress/gzip"
	"flag"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"strconv"
)

var (
	port        = flag.Int("port", 8989, "listen port")
	indexpath   = "/"
	lastUpdate  = "/lastupdate"
	trainer     = "/trainer"
	datadir     = "/data/"
	trainerFile = "trainer.yml"
)

func showIndex(w http.ResponseWriter, r *http.Request) {
	log.Printf("doing showIndex req: %+v\n", r)
	err := authenticate(r)
	if err != nil {
		http.Error(w, "auth fail", 401)
		return
	}

	rc := trainer + "\n" + lastUpdate + "\n"

	w.Write([]byte(rc))
}

func doGzip(a *[]byte) ([]byte, error) {
	var b bytes.Buffer
	gz := gzip.NewWriter(&b)
	if _, err := gz.Write(*a); err != nil {
		gz.Close()
		log.Printf("Error in gzip: %v", err)
		return nil, err
	}
	gz.Close()
	return b.Bytes(), nil
}

func authenticate(r *http.Request) error {
	user, pass, ok := r.BasicAuth()
	if !ok {
		// no credentials
		log.Printf("Basic auth not ok")
		return fmt.Errorf("Basic auth not ok")
	}
	// TODO, something more sophisticated
	if user == "mexf4ceuser555" && pass == "p4ssw0dmexf4c3999" {
		return nil
	}
	log.Printf("Basic auth failed")

	return fmt.Errorf("Basic auth failed")
}

func getLastUpdate(w http.ResponseWriter, r *http.Request) {
	log.Printf("doing getLastUpdate %+v\n", r)
	err := authenticate(r)
	if err != nil {
		http.Error(w, "auth fail", 401)
		return
	}
	info, err := os.Stat(datadir + trainerFile)

	if err != nil {
		http.Error(w, "cannot find trainer file", 500)
		return
	}
	rc := fmt.Sprintf("%d", info.ModTime().Unix())

	w.Write([]byte(rc))
}

func handleTrainer(w http.ResponseWriter, r *http.Request) {
	err := authenticate(r)
	if err != nil {
		http.Error(w, "auth fail", 401)
		return
	}
	if r.Method == "GET" {
		getTrainer(w, r)
	} else if r.Method == "POST" {
		updateTrainer(w, r)
	} else {
		http.Error(w, "unsupported", 405)
	}

}

func updateTrainer(w http.ResponseWriter, r *http.Request) {
	log.Printf("doing updateTrainer %+v\n", r)
	err := authenticate(r)
	if err != nil {
		http.Error(w, "auth fail", 401)
		return
	}

	file, _, err := r.FormFile("trainer")

	if err != nil {
		log.Printf("Error in file upload %v", err)
		http.Error(w, "unable to upload file", 400)
		return
	}
	defer file.Close()

	fileName := datadir + trainerFile
	//the file will never exist on startup
	ofile, err := os.Create(fileName)
	log.Printf("Writing file %s\n", fileName)
	defer ofile.Close()
	if err != nil {
		log.Printf("unable to write file: %s, err: %v\n", fileName, err)
		http.Error(w, "unable to write file", 400)
		return
	}
	io.Copy(ofile, file)
}

func getTrainer(w http.ResponseWriter, r *http.Request) {
	log.Printf("doing getTrainer %+v\n", r)

	var ims int64
	// see if there is a If-Modified-since header
	imshdr := r.Header.Get("If-Modified-Since")
	if imshdr != "" {
		var err error
		ims, err = strconv.ParseInt(imshdr, 10, 64)
		if err != nil {
			log.Printf("cannot parse If-Modified-Since %v", err)
			http.Error(w, "bad If-Modified-Since header", 400)
			return
		}
	}

	f, err := os.Open(datadir + trainerFile)
	if err != nil {
		log.Printf("file not found %s - %v\n", datadir+trainerFile, err)
		http.Error(w, "not found", 404)
		return
	}
	defer f.Close()
	fi, err := f.Stat()
	if err != nil {
		log.Printf("file stat failed %s - %v\n", datadir+trainerFile, err)
		http.Error(w, "file stat failed", 500)
		return
	}

	// if the If Modified header has a time which is earlier than the modified time of the
	// file then we return a not found
	if ims >= fi.ModTime().Unix() {
		log.Printf("not downloading file modify time %d not newer than If-Modified Since: %d\n", fi.ModTime().Unix(), ims)
		http.Error(w, "no newer file than If-Modified-Since", 404)
		return
	}

	filesize := fi.Size()
	buffer := make([]byte, filesize)

	_, err = f.Read(buffer)
	if err != nil {
		http.Error(w, "read failed", 500)
		return
	}

	gzipped, err := doGzip(&buffer)
	if err != nil {
		http.Error(w, "gzip failed", 500)
		return
	}
	updateTime := fmt.Sprintf("%d", fi.ModTime().Unix())
	w.Header().Set("Last-Modified", updateTime)
	w.Header().Set("Content-Length", fmt.Sprintf("%d", len(gzipped)))
	w.Header().Set("Content-Type", "application/x-yaml")
	w.Header().Set("Content-Encoding", "gzip")
	w.Write(gzipped)
}

func run() {
	http.HandleFunc(indexpath, showIndex)
	http.HandleFunc(trainer, handleTrainer)
	http.HandleFunc(lastUpdate, getLastUpdate)

	portstr := fmt.Sprintf(":%d", *port)

	log.Printf("Listening on http://127.0.0.1:%d", *port)
	if err := http.ListenAndServe(portstr, nil); err != nil {
		panic(err)
	}

}

func validateArgs() {
	flag.Parse()
	//nothing to check yet
}

func main() {
	validateArgs()
	run()
}
