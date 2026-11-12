export const getTileVarietyCount=(_level,maxAvailable)=>maxAvailable;
export const getOnetDifficulty=(input=1)=>{
  const level=Math.max(1,Math.floor(Number(input)||1));
  const tier=level<4?1:level<8?2:level<15?3:level<25?4:5;
  return {tier,label:['','Santai','Fokus','Menantang','Ahli','Master'][tier],
    groups:level<2?0:level<6?1:2,time:Math.max(75,150-(level-1)*2),
    separation:Math.min(8,2+Math.floor(level/3)),arrowPairs:level<8?0:Math.min(6,1+Math.floor((level-8)/4))};
};