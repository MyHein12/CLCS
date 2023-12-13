from faker import Faker
import random
import json

class Student:
    def __init__(self, name, age, major):
        self.name = name
        self.age = age
        self.major = major

def student_to_json(student):
    return {
        'name': student.name,
        'age': student.age,
        'major': student.major
    }

def generate_random_student():
    fake = Faker()
    name = fake.name()
    age = random.randint(18, 25)
    major = fake.job()
    return Student(name, age, major)

def create_student_sequences(num_sequences, sequence_length):
    student_sequences = []
    
    # Tạo chuỗi sinh viên đầu tiên
    sequence = [generate_random_student() for _ in range(sequence_length)]
    student_sequences.append(sequence)
    
    # Tạo các chuỗi sinh viên tiếp theo
    for _ in range(1, num_sequences):
        # Copy chuỗi sinh viên trước đó
        new_sequence = list(sequence)
        
        # Thay thế một số sinh viên ngẫu nhiên trong chuỗi
        num_replacements = random.randint(1, sequence_length // 2)  # Chọn một số sinh viên cần thay thế
        indices_to_replace = random.sample(range(sequence_length), num_replacements)  # Chọn vị trí cần thay thế
        
        for index in indices_to_replace:
            new_sequence[index] = generate_random_student()  # Thay thế sinh viên
        
        student_sequences.append(new_sequence)
        sequence = new_sequence  # Cập nhật chuỗi mới
        
    return student_sequences

def create_alphabet(student_sequences):
    alphabet = set()
    for sequence in student_sequences:
        for student in sequence:
            student_tuple = (student.name, student.age, student.major)
            if student_tuple not in alphabet:
                alphabet.add(student_tuple)
    return list(alphabet)

studentSequences = create_student_sequences(2, 5)

alphabet = create_alphabet(studentSequences)
print("Alphabet:")
for student_tuple in alphabet:
    print(f"Name: {student_tuple[0]}, Age: {student_tuple[1]}, Major: {student_tuple[2]}")


# Ghi dữ liệu sinh viên ra tệp tin JSON
with open('student_data.json', 'w') as file:
    data_to_write = {
        "studentSequences1": studentSequences[0],
        "studentSequences2": studentSequences[1]
    }
    json.dump(data_to_write, file, default=lambda o: o.__dict__, indent=4)
