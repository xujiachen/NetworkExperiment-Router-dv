class Record {
	public String dst;
	public String next;
	public Integer cost;
	public int count;
	public Record(String dst_, String next_, int cost_, int count_) {
		dst = dst_;
		next = next_;
		cost = cost_;
		count = count_;
	}
	public String toString() {
		return dst + " " + next + " " + cost + " " + count;
	}
}