import json
import os
import random

# Core seed patterns per intent with rich scanning and assistive domain depth
SEEDS = {
    "READ_TEXT": [
        "đọc chữ trên biển giúp tôi",
        "biển báo viết gì vậy",
        "trên tường có chữ gì",
        "đọc biển hiệu cửa hàng",
        "chữ trên bảng là gì",
        "có chữ gì trước mặt không",
        "đọc số phòng giúp tôi",
        "đọc tên tuyến xe buýt",
        "quét chữ giúp tôi",
        "đọc nhãn chai lọ này",
        "biển này ghi nội dung gì",
        "đọc giúp tôi văn bản phía trước",
        "xem chữ viết trên bảng chỉ dẫn",
        "đọc tờ giấy trước mặt",
        "biển báo giao thông này là gì",
        "đọc toa thuốc này giúp tôi",
        "thuốc này uống liều lượng bao nhiêu",
        "đọc tên thuốc trên vỉ này",
        "đọc hạn sử dụng hộp thuốc này",
        "hướng dẫn sử dụng thuốc này thế nào",
        "tờ tiền này là bao nhiêu tiền",
        "đọc mệnh giá tờ tiền trên tay tôi",
        "đây là tờ năm trăm nghìn hay năm mươi nghìn",
        "quét giúp tôi tờ tiền này",
        "tiền này mệnh giá bao nhiêu",
        "đọc hóa đơn thanh toán này",
        "hóa đơn này tổng cộng bao nhiêu tiền",
        "đọc menu quán ăn này giúp tôi",
        "quét thực đơn này xem có món gì",
        "đọc biển số xe buýt đến gần",
        "xe buýt tuyến số mấy đang tới",
        "đọc biển chỉ dẫn lối thoát hiểm",
        "đọc bảng tên đường phố trước mặt",
        "đọc số nhà trước mặt",
        "quét mã và đọc chữ trên bao bì",
        "chữ trên màn hình này là gì",
        "đọc lá thư này giúp tôi",
        "đọc tài liệu trên bàn",
        "đọc biển phòng khám bệnh viện",
        "đọc bảng thông báo ở sảnh",
        "đọc chữ in trên hộp sữa này",
        "nhận diện văn bản trên bức tường",
        "quét văn bản tiếng việt trước mặt",
        "đọc giùm tôi thông tin trên chai nước",
        "biển báo này cho phép đi hay cấm đi"
    ],
    "SAFETY_CHECK": [
        "có an toàn không",
        "tôi đi tiếp được không",
        "băng qua đường được chưa",
        "phía trước có xe không",
        "có nguy hiểm gì không",
        "bước tiếp có vấp không",
        "đường này đi được không",
        "tôi có thể qua đường không",
        "có chướng ngại vật trước mặt không",
        "có an toàn để bước đi không",
        "bước thêm một bước được không",
        "có hố sâu hay bậc thang không",
        "phía trước an toàn không bạn",
        "tôi qua đường lúc này được chưa",
        "đường đi có thông thoáng không",
        "có bậc tam cấp phía trước không",
        "có vũng nước cản đường không",
        "qua đường có xe máy chạy tới không",
        "bước xuống vỉa hè được chưa",
        "đường này có an toàn cho người khiếm thị không",
        "có xe ô tô đang lùi lại không",
        "phía trước có công trình thi công không",
        "đi thẳng có va vào cột điện không",
        "có dây điện rủ xuống không",
        "bước chân sang trái có an toàn không",
        "bước chân sang phải có vấp ngã không"
    ],
    "SURROUNDINGS_OBSERVE": [
        "xung quanh có những gì",
        "trước mặt tôi là gì",
        "camera thấy gì vậy",
        "quan sát giúp tôi xung quanh",
        "phía trước có người không",
        "bên cạnh có gì",
        "miêu tả không gian giúp tôi",
        "có gì ở bên trái không",
        "có gì ở bên phải không",
        "nhìn giúp tôi xem có gì",
        "có đồ vật nào cản đường không",
        "xem giúp tôi quang cảnh xung quanh",
        "bạn nhìn thấy những ai",
        "phía trước có chướng ngại gì",
        "xung quanh có đông người không",
        "tìm giúp tôi cái ghế ngồi",
        "có cái bàn nào gần đây không",
        "cửa ra vào ở hướng nào",
        "có cầu thang bộ ở đâu",
        "xung quanh có cây cối không",
        "tôi đang đứng trước cửa hàng nào",
        "có trạm xe buýt gần đây không",
        "xung quanh có vạch kẻ đường cho người đi bộ không",
        "nhìn xem có thùng rác ở gần không",
        "có ai đang đi lại gần tôi không"
    ],
    "SOUND_INQUIRY": [
        "tiếng gì kêu vậy bạn",
        "vừa có âm thanh gì thế",
        "nghe thấy tiếng gì không",
        "sao ồn ào quá vậy",
        "đo mức ồn giúp tôi",
        "tiếng động vừa rồi là gì",
        "xung quanh có tiếng người nói không",
        "có tiếng còi xe không",
        "âm thanh lạ phía trước là gì",
        "có tiếng chuông báo động không",
        "đây là tiếng xe cộ hay tiếng gì",
        "bạn nghe thấy gì không",
        "độ ồn hiện tại là bao nhiêu",
        "tiếng còi hú ở hướng nào",
        "xung quanh có tiếng động gì bất thường không",
        "có tiếng còi xe cứu thương không",
        "tiếng chuông cửa vừa reo phải không",
        "có tiếng gõ cửa không bạn",
        "vừa có tiếng kính vỡ ở đâu",
        "có tiếng la hét cầu cứu không",
        "tiếng còi xe vừa bấm ở bên nào",
        "đo cường độ decibel hiện tại",
        "môi trường xung quanh có yên tĩnh không"
    ],
    "HELP_REQUEST": [
        "cứu tôi với",
        "giúp tôi khẩn cấp",
        "tôi đang gặp nguy hiểm",
        "cần hỗ trợ khẩn cấp",
        "cứu với bạn ơi",
        "gọi người giúp tôi với",
        "tôi bị lạc đường rồi",
        "tôi bị ngã giúp tôi với",
        "nguy cấp quá",
        "báo động giúp tôi",
        "tôi cảm thấy không an toàn",
        "giúp đỡ khẩn cấp ngay",
        "tôi cần cứu hộ",
        "có ai cứu tôi không",
        "nguy hiểm quá giúp tôi",
        "tôi bị đau cần giúp đỡ",
        "phát tín hiệu cấp cứu khẩn cấp",
        "tôi mất phương hướng hoàn toàn",
        "gọi trợ giúp y tế",
        "bật còi báo động cứu hộ"
    ],
    "SITUATION_INQUIRY": [
        "tình hình thế nào rồi",
        "hiện tại tôi đang ở đâu",
        "tình huống xung quanh ra sao",
        "báo cáo tình hình hiện tại",
        "khu vực này là môi trường gì",
        "không gian này thế nào",
        "tình trạng hiện tại ra sao",
        "cập nhật tình hình giúp tôi",
        "tôi đang ở trong nhà hay ngoài đường",
        "khu vực xung quanh hiện tại thế nào",
        "tổng kết tình huống lúc này",
        "môi trường xung quanh có ổn định không",
        "hiện trạng xung quanh tôi như thế nào",
        "tóm tắt không gian quanh tôi"
    ],
    "FOLLOW_UP": [
        "còn bây giờ thì sao",
        "thế hiện tại thế nào",
        "lúc này thì sao rồi",
        "tiếp theo là gì",
        "vừa rồi thì sao",
        "bây giờ đã an toàn chưa",
        "tiếp tục thế nào",
        "có thay đổi gì không",
        "còn chướng ngại vật không",
        "bây giờ đi được chưa",
        "sau đó thì sao",
        "đã qua được chưa",
        "còn xe nào chạy qua nữa không",
        "lúc này đã yên tĩnh chưa",
        "vật cản đã rời đi chưa"
    ],
    "GREETING": [
        "xin chào",
        "chào bạn",
        "chào trợ lý",
        "chào sense ai",
        "alo bạn ơi",
        "hello bạn",
        "hi trợ lý",
        "có ai ở đó không",
        "bạn ơi",
        "chào buổi sáng",
        "chào buổi chiều",
        "chào buổi tối",
        "xin chào trợ lý ảo",
        "ê bạn ơi",
        "bạn có đó không",
        "chào sensebridge",
        "hello sense ai",
        "bạn sẵn sàng chưa",
        "bắt đầu nào bạn ơi",
        "mở camera lên nào"
    ],
    "SMALLTALK": [
        "cảm ơn bạn nhiều",
        "bạn tên là gì",
        "bạn là ai",
        "bạn thông minh quá",
        "cảm ơn trợ lý",
        "tạm biệt bạn",
        "hẹn gặp lại",
        "chúc một ngày tốt lành",
        "tuyệt vời lắm",
        "bạn làm việc rất tốt",
        "rất cảm ơn bạn",
        "bạn tên gì vậy",
        "ai tạo ra bạn",
        "tạm biệt nhé",
        "chúc bạn vui vẻ",
        "bạn giúp tôi nhiều lắm",
        "rất hữu ích cảm ơn nha",
        "tính năng này hay quá",
        "hẹn gặp lại trợ lý",
        "ngủ ngon nhé bạn"
    ],
    "GENERAL": [
        "nói gì đó đi",
        "bạn biết làm gì",
        "hướng dẫn sử dụng",
        "tôi có thể hỏi những gì",
        "bạn hỗ trợ được những tính năng nào",
        "giải thích giúp tôi",
        "bạn có nghe thấy tôi không",
        "thử nghiệm micro",
        "bạn có hiểu tôi nói không",
        "trợ lý có thể làm được gì",
        "các lệnh điều khiển bằng giọng nói",
        "hãy giới thiệu về bạn",
        "cách dùng ứng dụng sensebridge",
        "bật tính năng quét tự động",
        "tôi cần hướng dẫn sử dụng"
    ]
}

# Prefix variations
PREFIXES = [
    "", "xin ", "làm ơn ", "bạn ơi ", "trợ lý ơi ", "sense ai ơi ", "cho tôi hỏi ",
    "nhờ bạn ", "giúp tôi ", "hãy ", "vui lòng ", "alo ", "ê này ", "ê bạn ", "nhờ em "
]

# Suffix variations
SUFFIXES = [
    "", " giúp tôi", " giùm tôi", " hộ tôi", " với bạn", " nhé bạn", " nha bạn",
    " với", " nghen", " được không", " được chưa", " đi bạn", " vậy bạn",
    " ngay bây giờ", " xem sao", " giúp em", " với nhé", " giùm nha"
]

# Synonym replacement dictionary
SYNONYMS = {
    "đọc": ["quét", "xem", "nhận diện", "coi", "đọc giùm", "quét thử"],
    "biển báo": ["bảng hiệu", "tấm biển", "biển chỉ dẫn", "bảng thông báo"],
    "xe buýt": ["xe bus", "bus", "xe buýt công cộng"],
    "tiền": ["tờ tiền", "tiền mặt", "mệnh giá", "tờ giấy bạc"],
    "thuốc": ["toa thuốc", "đơn thuốc", "viên thuốc", "hộp thuốc"],
    "an toàn": ["ổn", "an tâm", "thông thoáng", "không có nguy hiểm"],
    "nguy hiểm": ["rủi ro", "tai nạn", "vấp ngã", "sự cố"],
    "xung quanh": ["quanh đây", "khu vực này", "không gian này", "môi trường quanh tôi"],
    "trước mặt": ["phía trước", "đằng trước", "thẳng phía trước"],
    "bên trái": ["phía bên trái", "tay trái", "hướng 9 giờ"],
    "bên phải": ["phía bên phải", "tay phải", "hướng 3 giờ"],
    "tiếng": ["âm thanh", "tiếng động", "tiếng kêu"],
    "cứu tôi": ["giúp tôi", "hỗ trợ tôi", "cấp cứu tôi"]
}

def generate_augmented_dataset(target_samples_per_intent=450):
    dataset = {"intents": []}
    total_samples = 0

    for tag, patterns in SEEDS.items():
        intent_patterns = set(patterns)

        # 1. Expand with prefixes and suffixes
        for p in list(patterns):
            for pre in PREFIXES:
                for suf in SUFFIXES:
                    combined = f"{pre}{p}{suf}".strip()
                    intent_patterns.add(combined)
                    if len(intent_patterns) >= target_samples_per_intent:
                        break
                if len(intent_patterns) >= target_samples_per_intent:
                    break

        # 2. Expand with synonym replacements
        while len(intent_patterns) < target_samples_per_intent:
            base = random.choice(patterns)
            words = base.split()
            new_words = []
            replaced = False
            for w in words:
                for k, v in SYNONYMS.items():
                    if k in base and not replaced:
                        replacement = random.choice(v)
                        new_words = base.replace(k, replacement).split()
                        replaced = True
                        break
            new_phrase = " ".join(new_words) if new_words else base
            pre = random.choice(PREFIXES)
            suf = random.choice(SUFFIXES)
            intent_patterns.add(f"{pre}{new_phrase}{suf}".strip())

        intent_list = sorted(list(intent_patterns))[:target_samples_per_intent]
        total_samples += len(intent_list)
        dataset["intents"].append({
            "tag": tag,
            "patterns": intent_list
        })
        print(f"Intent {tag}: {len(intent_list)} patterns generated.")

    print(f"\nTotal Dataset Samples: {total_samples}")
    return dataset

def main():
    dataset = generate_augmented_dataset(target_samples_per_intent=450)
    out_dir = os.path.join(os.path.dirname(__file__), "dataset")
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "intent_dataset_vi.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(dataset, f, ensure_ascii=False, indent=2)
    print(f"Successfully saved {out_path} ({os.path.getsize(out_path) / 1024:.1f} KB)")

if __name__ == "__main__":
    main()
