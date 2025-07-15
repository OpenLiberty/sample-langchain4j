db.createUser({
  user: "sampleUser",
  pwd: "openliberty",
  roles: [{ role: "readWrite", db: "embeddingsdb" }]
});

db.createCollection("Embeddings");
