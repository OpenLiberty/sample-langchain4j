db.createUser({
  user: "sampleUser",
  pwd: "openliberty",
  roles: [{ role: "readWrite", db: "testdb" }]
});

db.createCollection('EmbeddingsStored');
