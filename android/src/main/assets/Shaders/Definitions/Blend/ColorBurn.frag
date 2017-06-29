void main() {
    if(base.xyz == 0.0) {
        result = blend;
    } else {
        result = vec4(max((1.0 - ((1.0 - base) / blend)), 0.0)), base.w);
    }
}
