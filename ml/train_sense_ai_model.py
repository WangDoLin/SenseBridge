import json
import os
import re
import sys
import numpy as np
import tensorflow as tf

if sys.stdout.encoding != 'utf-8':
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

def clean_text(text):
    text = text.lower()
    text = re.sub(r'[^\w\s]', ' ', text)
    text = re.sub(r'\s+', ' ', text)
    return text.strip()

def build_vocab(patterns, max_vocab=800):
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

def text_to_sequence(text, vocab, max_len=24):
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

    print(f"Total training patterns: {len(patterns)}, Classes: {len(labels)}")

    vocab = build_vocab(patterns, max_vocab=800)
    max_len = 24
    X = np.array([text_to_sequence(p, vocab, max_len) for p in patterns], dtype=np.int32)
    y = np.array(y_raw, dtype=np.int32)

    # Shuffle dataset
    indices = np.arange(len(patterns))
    np.random.seed(42)
    np.random.shuffle(indices)
    X = X[indices]
    y = y[indices]

    split_idx = int(len(X) * 0.85)
    X_train, X_val = X[:split_idx], X[split_idx:]
    y_train, y_val = y[:split_idx], y[split_idx:]

    vocab_size = len(vocab)
    num_classes = len(labels)
    embedding_dim = 48

    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(max_len,), dtype=tf.int32),
        tf.keras.layers.Embedding(input_dim=vocab_size, output_dim=embedding_dim),
        tf.keras.layers.Conv1D(filters=64, kernel_size=3, padding='same', activation='relu'),
        tf.keras.layers.GlobalAveragePooling1D(),
        tf.keras.layers.Dense(96, activation="relu"),
        tf.keras.layers.Dropout(0.25),
        tf.keras.layers.Dense(48, activation="relu"),
        tf.keras.layers.Dense(num_classes, activation="softmax")
    ])

    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=0.003),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"]
    )

    print("\n--- Training Upgraded AI Scanner Model (4500 Samples) ---")
    history = model.fit(
        X_train, y_train,
        validation_data=(X_val, y_val),
        epochs=30,
        batch_size=32,
        verbose=1,
        shuffle=True
    )

    val_acc = history.history["val_accuracy"][-1]
    print(f"\nTraining completed. Validation accuracy: {val_acc * 100:.2f}%")

    # Convert to TFLite
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()

    assets_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets"))
    os.makedirs(assets_dir, exist_ok=True)

    tflite_path = os.path.join(assets_dir, "sense_ai_neural_model.tflite")
    with open(tflite_path, "wb") as f:
        f.write(tflite_model)
    print(f"Exported Optimized TFLite model: {tflite_path} ({len(tflite_model) / 1024:.1f} KB)")

    vocab_path = os.path.join(assets_dir, "sense_ai_vocab.json")
    with open(vocab_path, "w", encoding="utf-8") as f:
        json.dump(vocab, f, ensure_ascii=False, indent=2)
    print(f"Exported Vocab: {vocab_path} ({len(vocab)} words)")

    labels_path = os.path.join(assets_dir, "sense_ai_labels.json")
    with open(labels_path, "w", encoding="utf-8") as f:
        json.dump(labels, f, ensure_ascii=False, indent=2)
    print(f"Exported Labels: {labels_path} ({len(labels)} classes)")

    print("\n--- Benchmarking AI Scan & Interaction Queries ---")
    benchmark_queries = [
        "Đọc chữ trên biển giúp tôi",
        "Đọc liều dùng thuốc paracetamol này",
        "Tờ tiền này là bao nhiêu tiền",
        "Xe buýt tuyến số mấy đang tới vậy bạn",
        "Đọc hóa đơn thanh toán này",
        "Có qua đường an toàn không",
        "Xung quanh có những gì",
        "Có tiếng còi xe hú không",
        "Cứu tôi với bạn ơi",
        "Cảm ơn trợ lý nhiều nhé"
    ]
    for q in benchmark_queries:
        seq = np.array([text_to_sequence(q, vocab, max_len)], dtype=np.int32)
        preds = model.predict(seq, verbose=0)[0]
        best_idx = np.argmax(preds)
        confidence = preds[best_idx]
        print(f"Query: '{q}'\n  -> Predicted: {labels[best_idx]} ({confidence*100:.1f}%)\n")

if __name__ == "__main__":
    main()
