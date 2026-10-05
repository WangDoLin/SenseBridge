import json
import os
import re
import numpy as np
import tensorflow as tf

def clean_text(text):
    text = text.lower()
    text = re.sub(r'[^\w\s]', '', text)
    return text.strip()

def build_vocab(patterns, max_vocab=500):
    word_counts = {}
    for p in patterns:
        words = clean_text(p).split()
        for w in words:
            word_counts[w] = word_counts.get(w, 0) + 1
    
    sorted_words = sorted(word_counts.keys(), key=lambda w: word_counts[w], reverse=True)
    vocab = {"<PAD>": 0, "<UNK>": 1}
    for idx, w in enumerate(sorted_words[:max_vocab - 2], start=2):
        vocab[w] = idx
    return vocab

def text_to_sequence(text, vocab, max_len=20):
    words = clean_text(text).split()
    seq = [vocab.get(w, vocab["<UNK>"]) for w in words]
    if len(seq) < max_len:
        seq = seq + [vocab["<PAD>"]] * (max_len - len(seq))
    else:
        seq = seq[:max_len]
    return seq

def main():
    dataset_path = os.path.join(os.path.dirname(__file__), "dataset", "intent_dataset_vi.json")
    with open(dataset_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    labels = []
    patterns = []
    y_raw = []

    for item in data["intents"]:
        tag = item["tag"]
        if tag not in labels:
            labels.append(tag)
        tag_idx = labels.index(tag)
        for pattern in item["patterns"]:
            patterns.append(pattern)
            y_raw.append(tag_idx)

    print(f"Total patterns: {len(patterns)}, Classes: {len(labels)}")

    vocab = build_vocab(patterns, max_vocab=600)
    max_len = 20
    X = np.array([text_to_sequence(p, vocab, max_len) for p in patterns], dtype=np.int32)
    y = np.array(y_raw, dtype=np.int32)

    vocab_size = len(vocab)
    num_classes = len(labels)
    embedding_dim = 32

    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(max_len,), dtype=tf.int32),
        tf.keras.layers.Embedding(input_dim=vocab_size, output_dim=embedding_dim),
        tf.keras.layers.GlobalAveragePooling1D(),
        tf.keras.layers.Dense(64, activation="relu"),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(32, activation="relu"),
        tf.keras.layers.Dense(num_classes, activation="softmax")
    ])

    model.compile(
        optimizer="adam",
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"]
    )

    history = model.fit(
        X, y,
        epochs=45,
        batch_size=8,
        verbose=1,
        shuffle=True
    )

    final_acc = history.history["accuracy"][-1]
    print(f"Training completed. Final accuracy: {final_acc * 100:.2f}%")

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()

    assets_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets"))
    os.makedirs(assets_dir, exist_ok=True)

    tflite_path = os.path.join(assets_dir, "sense_ai_neural_model.tflite")
    with open(tflite_path, "wb") as f:
        f.write(tflite_model)
    print(f"Exported TFLite model: {tflite_path} ({len(tflite_model) / 1024:.1f} KB)")

    vocab_path = os.path.join(assets_dir, "sense_ai_vocab.json")
    with open(vocab_path, "w", encoding="utf-8") as f:
        json.dump(vocab, f, ensure_ascii=False, indent=2)
    print(f"Exported Vocab: {vocab_path} ({len(vocab)} words)")

    labels_path = os.path.join(assets_dir, "sense_ai_labels.json")
    with open(labels_path, "w", encoding="utf-8") as f:
        json.dump(labels, f, ensure_ascii=False, indent=2)
    print(f"Exported Labels: {labels_path} ({len(labels)} classes)")

    print("\n--- Testing Model Predictions ---")
    test_queries = [
        "Xin chào bạn",
        "Có qua đường an toàn không",
        "Đọc biển báo giúp tôi",
        "Có xe phía trước không",
        "Cứu tôi với",
        "Bạn tên là gì"
    ]
    for q in test_queries:
        seq = np.array([text_to_sequence(q, vocab, max_len)], dtype=np.int32)
        preds = model.predict(seq, verbose=0)[0]
        best_idx = np.argmax(preds)
        confidence = preds[best_idx]
        print(f"Query: '{q}' -> Predicted: {labels[best_idx]} ({confidence*100:.1f}%)")

if __name__ == "__main__":
    main()
