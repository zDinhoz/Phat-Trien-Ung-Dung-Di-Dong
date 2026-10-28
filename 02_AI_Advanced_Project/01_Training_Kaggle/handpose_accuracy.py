from __future__ import annotations
import numpy as np
import torch
from torch import nn
from torch.nn import functional as F
from torchvision.models import mobilenet_v2, MobileNet_V2_Weights
IMAGE_SIZE = 128
HEATMAP_SIZE = 64
MEAN = (0.485, 0.456, 0.406)
STD = (0.229, 0.224, 0.225)
FINGERS = {
    "thumb": [1, 2, 3, 4],
    "index": [5, 6, 7, 8],
    "middle": [9, 10, 11, 12],
    "ring": [13, 14, 15, 16],
    "pinky": [17, 18, 19, 20],
}



class ConvBNReLU(nn.Sequential):
    def __init__(self, cin, cout, kernel=3, groups=1):
        super().__init__(nn.Conv2d(cin, cout, kernel, padding=kernel//2, groups=groups, bias=False),
                         nn.BatchNorm2d(cout), nn.ReLU(inplace=False))
class Refine(nn.Sequential):
    def __init__(self, channels):
        super().__init__(ConvBNReLU(channels, channels, groups=channels),
                         ConvBNReLU(channels, channels, kernel=1),
                         ConvBNReLU(channels, channels, groups=channels),
                         ConvBNReLU(channels, channels, kernel=1))
class HandPoseAccuracy(nn.Module):
    def __init__(self, pretrained=False):
        super().__init__()
        features = mobilenet_v2(weights=MobileNet_V2_Weights.DEFAULT if pretrained else None).features
        self.stage64 = nn.Sequential(*[features[i] for i in range(0,2)])
        self.stage32 = nn.Sequential(*[features[i] for i in range(2,4)])
        self.stage16 = nn.Sequential(*[features[i] for i in range(4,7)])
        self.stage8 = nn.Sequential(*[features[i] for i in range(7,14)])
        self.stage4 = nn.Sequential(*[features[i] for i in range(14,18)])
        self.top = ConvBNReLU(320, 64, kernel=1)
        self.lat8 = ConvBNReLU(96,64,kernel=1)
        self.ref8 = Refine(64)
        self.lat16 = ConvBNReLU(32,64,kernel=1)
        self.ref16 = Refine(64)
        self.reduce = ConvBNReLU(64,48,kernel=1)
        self.lat32 = ConvBNReLU(24,48,kernel=1)
        self.ref32 = Refine(48)
        self.up64 = ConvBNReLU(48,32,kernel=1)
        self.lat64 = ConvBNReLU(16,32,kernel=1)
        self.ref64 = Refine(32)
        self.output = nn.Conv2d(32,21,1)
    def forward(self,x):
        s64 = self.stage64(x)
        s32 = self.stage32(s64)
        s16 = self.stage16(s32)
        s8 = self.stage8(s16)
        s4 = self.stage4(s8)
        x = self.ref8(F.interpolate(self.top(s4),scale_factor=2.,mode='nearest')+self.lat8(s8))
        x = self.ref16(F.interpolate(x,scale_factor=2.,mode='nearest')+self.lat16(s16))
        x = self.ref32(F.interpolate(self.reduce(x),scale_factor=2.,mode='nearest')+self.lat32(s32))
        x = self.ref64(F.interpolate(self.up64(x),scale_factor=2.,mode='nearest')+self.lat64(s64))
        return self.output(x)
def decode_heatmaps(logits):
    b,j,h,w=logits.shape
    index=logits.flatten(2).argmax(-1)
    return torch.stack(((index.remainder(w).float()+0.5)/w,
                        (index.div(w,rounding_mode='floor').float()+0.5)/h),dim=-1)
def model_summary():
    m=HandPoseAccuracy(pretrained=False)
    return {'parameters':sum(p.numel() for p in m.parameters()),'input_size':IMAGE_SIZE,'heatmap_size':HEATMAP_SIZE}
