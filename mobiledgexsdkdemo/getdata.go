package main

//very simple app which is similar to the example app, except this one supports HTTP GET

import (
	"bytes"
	"flag"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"strconv"
	"strings"
)

var (
	port        = flag.Int("port", 7777, "listen port")
	indexpath   = "/"
	getdatapath = "/getdata"
	getfilepath = "/getfile"
	filedir     = "/root/downloadfiles/"
)

func generateFiles() {
	if _, err := os.Stat(filedir); os.IsNotExist(err) {
		log.Printf("creating dir %s\n", filedir)
		err := os.Mkdir(filedir, 0755)
		if err != nil {
			log.Fatalf("unable to create %s -- %v\n", filedir, err)
		}
	}
	sizes := [4]uint32{1, 5, 10, 20}
	for _, s := range sizes {
		filename := fmt.Sprintf("%sdownload_%dMB.txt", filedir, s)

		//the file will never exist on startup
		ofile, err := os.Create(filename)
		log.Printf("Writing file %s\n", filename)
		defer ofile.Close()
		if err != nil {
			log.Fatalf("unable to create file: %s, err: %v\n", filename, err)
		}
		b := []byte("Z")
		contents := string(bytes.Repeat(b, 1024*1024*int(s)))
		fmt.Fprintf(ofile, contents)
	}
}

func showIndex(w http.ResponseWriter, r *http.Request) {
	log.Printf("doing showIndex req: %+v\n", r)
	rc := getdatapath + "\n" + getfilepath + "\n"

	w.Write([]byte(rc))
}

func getFile(w http.ResponseWriter, r *http.Request) {
	log.Printf("doing getFile %+v\n", r)
	filename := r.URL.Query().Get("filename")

	//do not allow any other paths
	if strings.Contains(filename, "/") {
		log.Printf("cannot specify directory %s\n", filename)
		http.Error(w, "bad request", 400)
		return
	}

	if filename == "" {
		log.Println("no filename")
		http.Error(w, "bad request", 400)
		return
	}

	f, err := os.Open(filedir + filename)
	if err != nil {
		log.Printf("not found file %s - %v\n", filename+filename, err)
		http.Error(w, "not found", 404)
		return
	}
	defer f.Close()
	fi, err := f.Stat()
	if err != nil {
		log.Printf("file stat failed %s - %v\n", filename+filename, err)
		http.Error(w, "file stat failed", 500)
		return
	}

	w.Header().Set("Content-Length", fmt.Sprintf("%d", fi.Size()))
	io.Copy(w, f)

}

func getData(w http.ResponseWriter, r *http.Request) {
	log.Printf("doing getData %+v\n", r)

	b := []byte("Z")
	numbytes := uint32(0)
	nb := r.URL.Query().Get("numbytes")

	if nb != "" {
		u, err := strconv.ParseUint(nb, 10, 32)
		if err == nil {
			numbytes = uint32(u)
		} else {
			log.Printf("Error in parseUint %v\n", err)
		}
	}
	response := string(bytes.Repeat(b, int(numbytes)))

	//force non chunked response
	w.Header().Set("Content-Length", nb)
	w.Write([]byte(response))
}

func run() {
	http.HandleFunc(indexpath, showIndex)
	http.HandleFunc(getdatapath, getData)
	http.HandleFunc(getfilepath, getFile)

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
	generateFiles()
	run()
}
