# Machine-learning foundations

NumPy represents fast multidimensional arrays; tensors extend that idea with accelerators and automatic differentiation. Pandas organizes labeled tables for cleaning and analysis. Pillow decodes/encodes everyday images. OpenCV supplies optimized geometry, filtering, feature and video operations. This backend needs only Pillow today.

A dataset contains examples and targets/metadata. Training adjusts neural-network weights to reduce a loss on training data. Validation guides choices on unseen samples; a final test set estimates generalization. Inference applies fixed weights to new input. Leakage, imbalance and distribution shift can make impressive metrics misleading.

Neural networks compose parameterized layers and nonlinearities. Convolutional networks historically dominated vision; vision transformers split images into patches and apply attention. PyTorch and TensorFlow are deep-learning frameworks for tensors, differentiation, training and deployment. PyTorch is common in research; TensorFlow has a broad production ecosystem. Neither is needed when calling a managed model API.

Transformers use attention to model relationships among token representations. Tokenization converts text to discrete tokens. Embeddings are learned vectors whose geometry captures useful similarity. LLMs generally predict tokens from context; instruction tuning and preference optimization make that base behavior more assistant-like. They can still hallucinate because fluent continuation is not database verification.

Computer vision extracts meaning from pixels: classification, detection, segmentation, pose, OCR and retrieval. Multimodal models map image and text representations into a shared generative/ reasoning system. CarVision uses multimodal inference, not a locally trained classifier. A rigorous future ML project would collect licensed, consented, diverse vehicle images; define make/model/generation labels; split by source/vehicle to prevent leakage; benchmark top-k accuracy and calibration; examine geography/lighting/modification failures; and version models/data.

Confidence from a generative model is not automatically calibrated probability. Preserve uncertainty, visual evidence and source provenance. Never infer safety, ownership, VIN, condition, exact engine or market price solely from appearance. Privacy work includes retention limits, access control, deletion, metadata stripping where appropriate and avoiding faces/plates unless necessary and lawful.
